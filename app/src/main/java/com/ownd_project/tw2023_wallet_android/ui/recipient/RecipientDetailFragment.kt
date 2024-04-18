package com.ownd_project.tw2023_wallet_android.ui.recipient

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentRecipientDetailBinding
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil


class RecipientDetailFragment : Fragment() {

    // navArgsデリゲートを使用して遷移元から引数を取得
    private val args: RecipientDetailFragmentArgs by navArgs()

    private var _binding: FragmentRecipientDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        DisplayUtil.setFragmentTitle(
            activity as? AppCompatActivity, getString(R.string.title_recipient)
        )

        _binding = FragmentRecipientDetailBinding.inflate(inflater, container, false)

        val recipientViewModel = ViewModelProvider(requireActivity())[RecipientViewModel::class.java]
        recipientViewModel.sharingHistories.observe(viewLifecycleOwner) { histories ->
            if (histories != null) {
                val historiesByRp = histories.itemsList.filter { it.rp == args.rp }
                if (historiesByRp.isNullOrEmpty()) {
                    Log.d(tag, "sharing histories that match the rp is empty")
                }else{
                    binding.histories.visibility = View.VISIBLE

                    val adapter = RecipientDetailAdapter(requireContext(), recipientViewModel, historiesByRp)
                    binding.histories.layoutManager = LinearLayoutManager(context)
                    binding.histories.adapter = adapter
                }
            }else{
                Log.d(tag, "empty sharing histories")
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val menuProvider = RecipientDetailFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val rpTextView = view.findViewById<TextView>(R.id.rp_text_view)
        rpTextView.text = args.rpName

        if (args.rpLocation != ""){
            val rpLocationHeader = view.findViewById<TextView>(R.id.rp_location_header)
            val rpLocation = view.findViewById<TextView>(R.id.rp_location)
            rpLocation.visibility = View.VISIBLE
            rpLocationHeader.visibility = View.VISIBLE
            rpLocation.text = args.rpLocation
        }
        if (args.rpContactUrl != ""){
            val rpContactHeader = view.findViewById<TextView>(R.id.rp_contact_header)
            val rpContact = view.findViewById<TextView>(R.id.rp_contact)
            rpContact.visibility = View.VISIBLE
            rpContactHeader.visibility = View.VISIBLE
            rpContact.text = args.rpContactUrl
        }
        if (args.rpPrivacyPolicyUrl != ""){
            val rpPrivacyPolicyHeader = view.findViewById<TextView>(R.id.rp_privacy_policy_header)
            val rpPrivacyPolicy = view.findViewById<TextView>(R.id.rp_privacy_policy)
            rpPrivacyPolicy.visibility = View.VISIBLE
            rpPrivacyPolicyHeader.visibility = View.VISIBLE
            rpPrivacyPolicy.text = args.rpPrivacyPolicyUrl
        }
        if (args.rpLogoUrl != ""){
            val rpLogoImageView = view.findViewById<ImageView>(R.id.rp_logo_image)
            rpLogoImageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(args.rpLogoUrl)
                .into(rpLogoImageView)
        }
    }
}



class RecipientDetailAdapter(
    private val context: Context,
    private val recipientViewModel: RecipientViewModel,
    private val histories: List<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory>) :
    RecyclerView.Adapter<RecipientDetailAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val provideDate: TextView = view.findViewById(R.id.provide_date)
        val claims: TextView = view.findViewById(R.id.claims)
        val numberOfClaims: TextView = view.findViewById(R.id.number_of_claims)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = histories[position]
        val claims = history.claimsList
        val claimsText = concatenateAndTruncate(claims, 15)
        val numberOfClaims = context.getString(R.string.number_of_claims, claims.size.toString())

        holder.provideDate.text = timestampToString(history.createdAt)
        holder.claims.text = claimsText
        holder.numberOfClaims.text = numberOfClaims

        holder.itemView.setOnClickListener {
            recipientViewModel.setTargetHistory(history)
            val action = RecipientDetailFragmentDirections.actionToSharedClaimDetail()
            it.findNavController().navigate(action)
        }
    }

    private fun Bundle.putCredentialSharingHistory(
        key: String,
        credentialSharingHistory: com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory,
    ) {
        putByteArray(key, credentialSharingHistory.toByteArray())
    }

    override fun getItemCount() = histories.size
}