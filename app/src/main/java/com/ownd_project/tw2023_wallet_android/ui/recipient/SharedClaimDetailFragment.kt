package com.ownd_project.tw2023_wallet_android.ui.recipient

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentSharedClaimDetailBinding
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil

class SharedClaimDetailFragment : Fragment() {

    private var _binding: FragmentSharedClaimDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSharedClaimDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val menuProvider = SharedClaimDetailFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val recipientViewModel = ViewModelProvider(requireActivity())[RecipientViewModel::class.java]

        recipientViewModel.targetHistory.observe(viewLifecycleOwner) { history ->
            if (history != null) {
                val timestampInString = timestampToString(history.createdAt)
                DisplayUtil.setFragmentTitle(
                    activity as? AppCompatActivity, getString(
                        R.string.timing_of_claim_sharing, timestampInString
                    )
                )

                val rpTextView = view.findViewById<TextView>(R.id.claim_recipient)
                rpTextView.text = getString(R.string.claim_recipient, history.rp)

                val adapter = ClaimAdapter(history.claimsList)
                binding.sharedClaims.layoutManager = LinearLayoutManager(context)
                binding.sharedClaims.adapter = adapter
            }else{
                Log.d(tag, "unset target history!")
            }
        }
    }
}





class ClaimAdapter(
    private val claims: List<String>) :
    RecyclerView.Adapter<ClaimAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val claimTitle: TextView = view.findViewById(R.id.claim_title)
        val claimDescription: TextView = view.findViewById(R.id.claim_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_claim, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val claim = claims[position]
        holder.claimTitle.text = claim
    }

    private fun Bundle.putCredentialSharingHistory(
        key: String,
        claim: String,
    ) {
        putByteArray(key, claim.toByteArray())
    }

    override fun getItemCount() = claims.size
}